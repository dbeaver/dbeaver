/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.project.DBPResourceHandler;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceRegistry;

public class SQLEditorInputFactory implements IElementFactory
{
    static final Log log = LogFactory.getLog(SQLEditorInputFactory.class);

    private static final String ID_FACTORY = "org.jkiss.dbeaver.ui.editors.sql.SQLEditorInputFactory"; //$NON-NLS-1$

    private static final String TAG_PATH = "path"; //$NON-NLS-1$
    private static final String TAG_NAME = "name"; //$NON-NLS-1$
    private static final String TAG_PROJECT = "project"; //$NON-NLS-1$
    private static final String TAG_DATA_SOURCE = "data-source"; //$NON-NLS-1$

    public SQLEditorInputFactory()
    {
    }

    public IAdaptable createElement(IMemento memento)
    {
        // Get the file name.
        String fileName = memento.getString(TAG_PATH);
        if (fileName == null) {
            return null;
        }

        // Get a handle to the IFile...which can be a handle
        // to a resource that does not exist in workspace
        IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(fileName));
        if (file != null) {
            DataSourceDescriptor dataSource = null;
            String projectId = memento.getString(TAG_PROJECT);
            if (projectId != null) {
                final IProject project = DBeaverCore.getInstance().getProject(projectId);
                if (project != null) {
                    DataSourceRegistry registry = DBeaverCore.getInstance().getProjectRegistry().getDataSourceRegistry(project);
                    if (registry != null) {
                        String dataSourceId = memento.getString(TAG_DATA_SOURCE);
                        if (dataSourceId != null) {
                            dataSource = registry.getDataSource(dataSourceId);
                            if (dataSource == null) {
                                log.warn("Can't find datasource '" + dataSourceId + "' for file '" + fileName + "'");
                            }
                        }
                    }
                }
            }
            String scriptName = memento.getString(TAG_NAME);
            if (scriptName == null) {
                scriptName = "";
            }
            return new SQLEditorInput(file, dataSource, scriptName);
        }
        return null;
    }

    public static String getFactoryId()
    {
        return ID_FACTORY;
    }

    public static void saveState(IMemento memento, SQLEditorInput input)
    {
        IFile file = input.getFile();
        memento.putString(TAG_PATH, file.getFullPath().toString());
        memento.putString(TAG_NAME, input.getScriptName());
        if (input.getDataSourceContainer() != null) {
            memento.putString(TAG_DATA_SOURCE, input.getDataSourceContainer().getId());
            try {
                memento.putString(TAG_PROJECT, input.getDataSourceContainer().getRegistry().getProject().getPersistentProperty(DBPResourceHandler.PROP_PROJECT_ID));
            } catch (CoreException e) {
                log.warn(e);
            }
        }
    }
}